%define __jar_repack {%nil}
%define _prefix %{_usr}/local/chronopolis/ingest
%define jar ingest-server.jar
%define yaml application.yml
%define initsh /etc/init.d/ingest-server
%define build_time %(date +"%Y%m%d")

Name: ingest-server
Version: %{ver}
Release: %{build_time}.el6
Source: ingest-server.jar
Source1: ingest-server.sh
Source2: ingest-application.yml
Summary: Chronopolis Ingest Server
License: BSD-3
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
Requires: postgresql-server >= 8.1
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Ingest Server hosts the API for handling bags, transfers, and
tokens.

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"
%__install -D -m0755 "%{SOURCE1}" "%{buildroot}%{initsh}"

%files

%defattr(-,root,root)
%dir %{_prefix}
%{_prefix}/%{jar}
%config(noreplace) %{_prefix}/%{yaml}
%{initsh}

%post

chkconfig --add ingest-server

%preun

chkconfig --del ingest-server

%changelog

* Tue Mar 5 2019 Mike Ritter <shake@umiacs.umd.edu> 3.1.0-20190305
- Set license to BSD 3 clause

* Wed Nov 8 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.3-20171108
- remove install command for logging directory

* Mon Oct 2 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171002
- added changelog entry
- update mod for application yaml
